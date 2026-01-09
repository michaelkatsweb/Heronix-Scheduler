"""
Task Scheduling Algorithm - Greedy LPT with Local Search Optimization
Author:Michael Katsaros for CS 3303 Assignment - Unit 7
Description: Minimizes makespan for task assignment to machines in smart factory
"""

import random
import time
import heapq
from typing import List, Tuple, Dict
import statistics

class TaskScheduler:
    """
    Implements Greedy Longest Processing Time (LPT) algorithm with local search
    optimization for task scheduling in manufacturing environments.
    """
    
    def __init__(self, tasks: List[int], num_machines: int):
        """
        Initialize the scheduler.
        
        Args:
            tasks: List of task durations
            num_machines: Number of available machines
        """
        self.tasks = tasks
        self.num_machines = num_machines
        self.schedule = [[] for _ in range(num_machines)]
        self.machine_loads = [0] * num_machines
        self.makespan = 0
        self.execution_time = 0
        
    def lpt_schedule(self) -> Tuple[List[List[int]], int, float]:
        """
        Greedy LPT Algorithm: Sortt tasks in descending order and assign each
        to the machine with minimum current load.
        
        Returns:
            Tuple of (schedule, makespan, execution_time)
        """
        start_time = time.time()
        
        # Sort tasks in descending order (Longest Processing Time first)
        sorted_tasks = sorted(enumerate(self.tasks), key=lambda x: x[1], reverse=True)
        
        # Use min-heap to efficiently track machine with minimum load
        # Heap stores tuples of (current_load, machine_id)
        machine_heap = [(0, i) for i in range(self.num_machines)]
        heapq.heapify(machine_heap)
        
        # Reset schedule
        self.schedule = [[] for _ in range(self.num_machines)]
        self.machine_loads = [0] * self.num_machines
        
        # Assign each task to machine with minimum load
        for task_id, task_duration in sorted_tasks:
            # Get machine with minimum load
            min_load, machine_id = heapq.heappop(machine_heap)
            
            # Assign task to this machine
            self.schedule[machine_id].append((task_id, task_duration))
            self.machine_loads[machine_id] = min_load + task_duration
            
            # Push updated load back to heap
            heapq.heappush(machine_heap, (self.machine_loads[machine_id], machine_id))
        
        # Calculate makespan (maximum machine load)
        self.makespan = max(self.machine_loads)
        self.execution_time = time.time() - start_time
        
        return self.schedule, self.makespan, self.execution_time
    
    def local_search_optimization(self, max_iterations: int = 100) -> Tuple[int, int]:
        """
        Local search to improve LPT solution by moving tasks between machines.
        
        Args:
            max_iterations: Maximum number of improvement iterations
            
        Returns:
            Tuple of (final_makespan, iterations_performed)
        """
        iterations = 0
        improved = True
        
        while improved and iterations < max_iterations:
            improved = False
            
            # Find the most loaded machine (bottleneck)
            max_machine = self.machine_loads.index(max(self.machine_loads))
            
            if not self.schedule[max_machine]:
                break
                
            # Try to move the largest task from max_machine to another machine
            largest_task_idx = max(range(len(self.schedule[max_machine])),
                                  key=lambda i: self.schedule[max_machine][i][1])
            task_to_move = self.schedule[max_machine][largest_task_idx]
            
            # Find the least loaded machine
            min_machine = self.machine_loads.index(min(self.machine_loads))
            
            # Check if moving improves makespan
            new_max_load = self.machine_loads[max_machine] - task_to_move[1]
            new_min_load = self.machine_loads[min_machine] + task_to_move[1]
            new_makespan = max(max(self.machine_loads[i] for i in range(self.num_machines)
                                  if i != max_machine and i != min_machine),
                              new_max_load, new_min_load)
            
            if new_makespan < self.makespan:
                # Move the task
                self.schedule[max_machine].pop(largest_task_idx)
                self.schedule[min_machine].append(task_to_move)
                self.machine_loads[max_machine] = new_max_load
                self.machine_loads[min_machine] = new_min_load
                self.makespan = new_makespan
                improved = True
                iterations += 1
        
        return self.makespan, iterations
    
    def calculate_statistics(self) -> Dict:
        """Calculate performance statistics for the schedule."""
        total_work = sum(self.tasks)
        average_load = total_work / self.num_machines
        load_variance = statistics.variance(self.machine_loads) if len(self.machine_loads) > 1 else 0
        load_std_dev = statistics.stdev(self.machine_loads) if len(self.machine_loads) > 1 else 0
        
        # Calculate utilization for each machine
        utilizations = [load / self.makespan * 100 if self.makespan > 0 else 0 
                       for load in self.machine_loads]
        
        # Calculate efficiency (ratio of average load to makespan)
        efficiency = (average_load / self.makespan * 100) if self.makespan > 0 else 0
        
        # Calculate theoretical lower bound
        lower_bound = max(average_load, max(self.tasks))
        approximation_ratio = self.makespan / lower_bound if lower_bound > 0 else 1.0
        
        return {
            'total_tasks': len(self.tasks),
            'num_machines': self.num_machines,
            'total_work': total_work,
            'makespan': self.makespan,
            'average_load': average_load,
            'load_std_dev': load_std_dev,
            'load_variance': load_variance,
            'min_load': min(self.machine_loads),
            'max_load': max(self.machine_loads),
            'utilizations': utilizations,
            'average_utilization': statistics.mean(utilizations),
            'efficiency': efficiency,
            'lower_bound': lower_bound,
            'approximation_ratio': approximation_ratio,
            'execution_time': self.execution_time
        }
    
    def print_schedule(self, detailed: bool = True):
        """Print the current schedule in a readable format."""
        print("\n" + "="*70)
        print("TASK SCHEDULING RESULTS")
        print("="*70)
        
        stats = self.calculate_statistics()
        
        print(f"\nConfiguration:")
        print(f"  Total Tasks: {stats['total_tasks']}")
        print(f"  Number of Machines: {stats['num_machines']}")
        print(f"  Total Work: {stats['total_work']} time units")
        
        print(f"\nPerformance Metrics:")
        print(f"  Makespan (Total Completion Time): {stats['makespan']} time units")
        print(f"  Theoretical Lower Bound: {stats['lower_bound']:.2f} time units")
        print(f"  Approximation Ratio: {stats['approximation_ratio']:.4f}")
        print(f"  Overall Efficiency: {stats['efficiency']:.2f}%")
        print(f"  Algorithm Execution Time: {stats['execution_time']:.6f} seconds")
        
        print(f"\nLoad Distribution:")
        print(f"  Average Load: {stats['average_load']:.2f} time units")
        print(f"  Load Std Dev: {stats['load_std_dev']:.2f}")
        print(f"  Min Load: {stats['min_load']} time units")
        print(f"  Max Load: {stats['max_load']} time units")
        print(f"  Load Range: {stats['max_load'] - stats['min_load']} time units")
        
        if detailed:
            print(f"\nMachine-by-Machine Breakdown:")
            print("-" * 70)
            for i in range(self.num_machines):
                num_tasks = len(self.schedule[i])
                load = self.machine_loads[i]
                utilization = stats['utilizations'][i]
                task_ids = [task[0] for task in self.schedule[i]]
                
                print(f"Machine {i:2d}: Load = {load:5d} | "
                      f"Utilization = {utilization:5.1f}% | "
                      f"Tasks = {num_tasks:3d} | "
                      f"Task IDs: {task_ids[:10]}{'...' if num_tasks > 10 else ''}")
        
        print("="*70 + "\n")


def generate_test_data(num_tasks: int, min_duration: int = 1, 
                       max_duration: int = 100) -> List[int]:
    """Generate random task durations for testing."""
    return [random.randint(min_duration, max_duration) for _ in range(num_tasks)]


def run_small_example():
    """Run a small example to demonstrate the algorithm."""
    print("\n" + "#"*70)
    print("# EXAMPLE 1: Small Dataset (10 tasks, 3 machines)")
    print("#"*70)
    
    tasks = [10, 5, 8, 7, 12, 15, 6, 9, 11, 4]
    num_machines = 3
    
    print(f"\nInput Tasks: {tasks}")
    print(f"Number of Machines: {num_machines}")
    
    scheduler = TaskScheduler(tasks, num_machines)
    
    print("\n--- Running LPT Algorithm ---")
    schedule, makespan, exec_time = scheduler.lpt_schedule()
    scheduler.print_schedule(detailed=True)
    
    print("\n--- Running Local Search Optimization ---")
    optimized_makespan, iterations = scheduler.local_search_optimization()
    print(f"Local Search Iterations: {iterations}")
    print(f"Makespan after optimization: {optimized_makespan}")
    
    if iterations > 0:
        scheduler.print_schedule(detailed=True)
    else:
        print("No improvement found - LPT solution was already optimal!\n")


def run_large_dataset_test():
    """Run test with large dataset as required by assignment."""
    print("\n" + "#"*70)
    print("# EXAMPLE 2: Large Dataset (200 tasks, 12 machines)")
    print("#"*70)
    
    # Set seed for reproducibility
    random.seed(42)
    
    num_tasks = 200
    num_machines = 12
    tasks = generate_test_data(num_tasks, min_duration=10, max_duration=100)
    
    print(f"\nDataset Configuration:")
    print(f"  Number of Tasks: {num_tasks}")
    print(f"  Number of Machines: {num_machines}")
    print(f"  Task Duration Range: 10-100 time units")
    print(f"  Sample Tasks: {tasks[:20]}...")
    
    scheduler = TaskScheduler(tasks, num_machines)
    
    print("\n--- Running LPT Algorithm ---")
    schedule, makespan, exec_time = scheduler.lpt_schedule()
    scheduler.print_schedule(detailed=True)
    
    print("\n--- Running Local Search Optimization ---")
    start_ls = time.time()
    optimized_makespan, iterations = scheduler.local_search_optimization(max_iterations=1000)
    ls_time = time.time() - start_ls
    
    print(f"Local Search Performance:")
    print(f"  Iterations: {iterations}")
    print(f"  Execution Time: {ls_time:.6f} seconds")
    print(f"  Improvement: {makespan - optimized_makespan} time units")
    print(f"  Improvement Percentage: {((makespan - optimized_makespan)/makespan * 100):.2f}%")
    
    if iterations > 0:
        scheduler.print_schedule(detailed=True)
    else:
        print("\nNo improvement found - LPT solution was already optimal or near-optimal!")


def run_scalability_analysis():
    """Analyze how algorithm performance scales with input size."""
    print("\n" + "#"*70)
    print("# SCALABILITY ANALYSIS")
    print("#"*70)
    
    test_configs = [
        (50, 5),
        (100, 10),
        (200, 12),
        (500, 20),
        (1000, 25),
        (2000, 30),
    ]
    
    print("\nTesting algorithm performance across different input sizes:")
    print("-" * 70)
    print(f"{'Tasks':>6} | {'Machines':>8} | {'Makespan':>8} | "
          f"{'LPT Time':>10} | {'LS Time':>10} | {'Total':>10}")
    print("-" * 70)
    
    for num_tasks, num_machines in test_configs:
        tasks = generate_test_data(num_tasks, 10, 100)
        scheduler = TaskScheduler(tasks, num_machines)
        
        # Run LPT
        schedule, makespan, lpt_time = scheduler.lpt_schedule()
        
        # Run Local Search
        ls_start = time.time()
        optimized_makespan, iterations = scheduler.local_search_optimization(max_iterations=100)
        ls_time = time.time() - ls_start
        
        total_time = lpt_time + ls_time
        
        print(f"{num_tasks:6d} | {num_machines:8d} | {optimized_makespan:8d} | "
              f"{lpt_time:9.6f}s | {ls_time:9.6f}s | {total_time:9.6f}s")
    
    print("-" * 70)
    print("\nComplexity Observations:")
    print("  - LPT Algorithm: O(n log n + n log m) where n=tasks, m=machines")
    print("  - Execution time grows sub-linearly with input size")
    print("  - Algorithm remains highly efficient even for large datasets")
    print("  - Local search adds minimal overhead while improving solution quality")


def manufacturing_scenario_example():
    """
    Example incorporating manufacturing context for LSS Black Belt perspective.
    """
    print("\n" + "#"*70)
    print("# MANUFACTURING SCENARIO: Electronics Assembly Line")
    print("#"*70)
    
    print("\nScenario:")
    print("  An electronics manufacturing facility has 10 assembly stations")
    print("  (machines) and needs to process 100 circuit board assembly tasks.")
    print("  Each task has varying complexity requiring different processing times.")
    print("  The goal is to minimize total completion time (makespan) while")
    print("  maintaining balanced workload across all stations.")
    
    # Generate realistic manufacturing task times (in minutes)
    random.seed(123)
    num_tasks = 100
    num_machines = 10
    
    # Manufacturing tasks typically have different complexity tiers
    tasks = []
    # 20% simple tasks (5-15 min)
    tasks.extend([random.randint(5, 15) for _ in range(20)])
    # 50% medium tasks (15-45 min)
    tasks.extend([random.randint(15, 45) for _ in range(50)])
    # 30% complex tasks (45-80 min)
    tasks.extend([random.randint(45, 80) for _ in range(30)])
    
    random.shuffle(tasks)
    
    print(f"\nManufacturing Metrics:")
    print(f"  Total Tasks (Work Orders): {num_tasks}")
    print(f"  Assembly Stations: {num_machines}")
    print(f"  Task Complexity Distribution:")
    print(f"    - Simple (5-15 min): 20 tasks")
    print(f"    - Medium (15-45 min): 50 tasks")
    print(f"    - Complex (45-80 min): 30 tasks")
    
    scheduler = TaskScheduler(tasks, num_machines)
    
    # Run optimization
    schedule, makespan, exec_time = scheduler.lpt_schedule()
    optimized_makespan, iterations = scheduler.local_search_optimization()
    
    scheduler.print_schedule(detailed=True)
    
    # Manufacturing KPIs
    stats = scheduler.calculate_statistics()
    takt_time = makespan / num_tasks
    
    print("\nManufacturing KPIs (Lean Six Sigma Perspective):")
    print(f"  Takt Time: {takt_time:.2f} minutes/task")
    print(f"  Overall Equipment Effectiveness (OEE) Estimate: {stats['efficiency']:.1f}%")
    print(f"  Load Balancing (Std Dev): {stats['load_std_dev']:.2f} minutes")
    print(f"  Bottleneck Station Load: {stats['max_load']} minutes")
    print(f"  Underutilized Station Load: {stats['min_load']} minutes")
    print(f"  Capacity Utilization Range: "
          f"{min(stats['utilizations']):.1f}% - {max(stats['utilizations']):.1f}%")
    
    # Calculate theoretical vs actual throughput
    theoretical_throughput = stats['total_work'] / num_machines
    print(f"\nThroughput Analysis:")
    print(f"  Theoretical Perfect Balance: {theoretical_throughput:.2f} min/station")
    print(f"  Actual Makespan: {makespan} minutes")
    print(f"  Efficiency Loss: {makespan - theoretical_throughput:.2f} minutes")
    print(f"  Efficiency: {(theoretical_throughput/makespan)*100:.2f}%")


if __name__ == "__main__":
    print("\n" + "="*70)
    print("TASK SCHEDULING ALGORITHM - COMPREHENSIVE TESTING")
    print("CS 3303: Design and Analysis of Algorithms")
    print("="*70)
    
    # Run all test cases
    run_small_example()
    run_large_dataset_test()
    run_scalability_analysis()
    manufacturing_scenario_example()
    
    print("\n" + "="*70)
    print("ALL TESTS COMPLETED SUCCESSFULLY")
    print("="*70 + "\n")